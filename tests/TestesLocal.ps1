$ErrorActionPreference = 'Stop'
$basea = 'http://127.0.0.1:8080'

function iCheck([string]$name_a, [bool]$condition_a) {
    if (-not $condition_a) {
        throw "FAIL: $name_a"
    }
    Write-Output "PASS: $name_a"
}

function iReadResponse($stream_a) {
    $header_a = ''
    while (-not $header_a.EndsWith("`r`n`r`n")) {
        $byte_a = $stream_a.ReadByte()
        if ($byte_a -lt 0) {
            throw 'EOF antes do fim dos cabeçalhos'
        }
        $header_a += [char]$byte_a
        if ($header_a.Length -gt 32768) {
            throw 'Cabeçalho de resposta excessivo'
        }
    }

    $lengthLine_a = $header_a -split "`r`n" |
        Where-Object { $_ -match '^Content-Length: \d+$' } |
        Select-Object -First 1
    $length_a = [int]($lengthLine_a -replace '^Content-Length: ', '')
    $body_a = New-Object System.Collections.Generic.List[byte]
    for ($index_a = 0; $index_a -lt $length_a; $index_a++) {
        $byte_a = $stream_a.ReadByte()
        if ($byte_a -lt 0) {
            throw 'EOF antes do fim do corpo'
        }
        $body_a.Add([byte]$byte_a)
    }
    return @{ Header = $header_a; Body = $body_a.ToArray() }
}

function iReadHeaders($stream_a) {
    $header_a = ''
    while (-not $header_a.EndsWith("`r`n`r`n")) {
        $byte_a = $stream_a.ReadByte()
        if ($byte_a -lt 0) {
            throw 'EOF antes do fim dos cabeçalhos'
        }
        $header_a += [char]$byte_a
        if ($header_a.Length -gt 32768) {
            throw 'Cabeçalho de resposta excessivo'
        }
    }
    return $header_a
}

function iRunJava([string]$arguments_a) {
    $startInfo_a = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo_a.FileName = 'java.exe'
    $startInfo_a.Arguments = $arguments_a
    $startInfo_a.UseShellExecute = $false
    $startInfo_a.RedirectStandardError = $true
    $startInfo_a.CreateNoWindow = $true
    $process_a = [System.Diagnostics.Process]::Start($startInfo_a)
    $errorText_a = $process_a.StandardError.ReadToEnd()
    $process_a.WaitForExit()
    return @{ ExitCode = $process_a.ExitCode; Error = $errorText_a }
}

$geta = (& curl.exe --http1.1 -sS -i "$basea/teste.txt" | Out-String)
iCheck 'GET retorna 200' ($geta -match '^HTTP/1\.1 200 OK')
iCheck 'Date, Server e Content-Type' (($geta -match '(?m)^Server: Grupo10-HTTPServer\r?$') -and ($geta -match '(?m)^Date: \w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} GMT\r?$') -and ($geta -match '(?m)^Content-Type: text/plain; charset=utf-8\r?$'))
$actualLengtha = [int]([regex]::Match($geta, '(?im)^Content-Length: (\d+)').Groups[1].Value)
$diskLengtha = [IO.File]::ReadAllBytes((Resolve-Path 'src\www\teste.txt')).Length
iCheck 'Content-Length corresponde aos bytes GET' ($actualLengtha -eq $diskLengtha)
$missingArgs_a = iRunJava '-cp out Servidor.Server'
iCheck 'Argumentos obrigatórios são validados' (($missingArgs_a.ExitCode -ne 0) -and ($missingArgs_a.Error -match 'Informe --port e --root'))
$missingRoot_a = iRunJava '-cp out Servidor.Server --port 8080 --root .\raiz-que-nao-existe'
iCheck 'Diretório raiz inexistente é rejeitado' (($missingRoot_a.ExitCode -ne 0) -and ($missingRoot_a.Error -match 'Falha ao iniciar'))

$heada = (& curl.exe --http1.1 -sS -I "$basea/teste.txt" | Out-String)
$headLengtha = [int]([regex]::Match($heada, '(?im)^Content-Length: (\d+)').Groups[1].Value)
iCheck 'HEAD retorna 200 sem corpo e com tamanho do GET' (($heada -match '^HTTP/1\.1 200 OK') -and ($headLengtha -eq $actualLengtha))
$headClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$headClient_a.ReceiveTimeout = 3000
$headStream_a = $headClient_a.GetStream()
$headRequest_a = [Text.Encoding]::ASCII.GetBytes("HEAD /teste.txt HTTP/1.1`r`nHost: localhost`r`nConnection: close`r`n`r`n")
$headStream_a.Write($headRequest_a, 0, $headRequest_a.Length)
$headStream_a.Flush()
$headRaw_a = iReadHeaders $headStream_a
try { $headBodyByte_a = $headStream_a.ReadByte() } catch [System.IO.IOException] { $headBodyByte_a = -1 }
iCheck 'HEAD raw não envia nenhum byte de corpo' (($headRaw_a -match '^HTTP/1\.1 200 OK') -and ($headBodyByte_a -eq -1))
$headClient_a.Dispose()

$notFounda = (& curl.exe --http1.1 -sS -i "$basea/nao-existe-123.txt" | Out-String)
iCheck 'Arquivo inexistente retorna 404' ($notFounda -match '^HTTP/1\.1 404 Not Found')
$methoda = (& curl.exe --http1.1 -sS -i -X POST "$basea/teste.txt" | Out-String)
iCheck 'Método não suportado retorna 405 e Allow' (($methoda -match '^HTTP/1\.1 405 Method Not Allowed') -and ($methoda -match '(?im)^Allow: GET, HEAD'))
$queryResponse_a = (& curl.exe --http1.1 -sS -i "$basea/teste.txt?modo=consulta" | Out-String)
iCheck 'Query string não é tratada como parte do nome do arquivo' ($queryResponse_a -match '^HTTP/1\.1 200 OK')
$dotNameResponse_a = (& curl.exe --http1.1 -sS -i "$basea/..nota.txt" | Out-String)
iCheck 'Nome legítimo iniciado por pontos permanece acessível' ($dotNameResponse_a -match '^HTTP/1\.1 200 OK')
$travel1a = (& curl.exe --path-as-is --http1.1 -sS -i "$basea/../../Windows/System32/drivers/etc/hosts" | Out-String)
$travel2a = (& curl.exe --path-as-is --http1.1 -sS -i "$basea/%2e%2e/%2e%2e/Windows/System32/drivers/etc/hosts" | Out-String)
$travel3a = (& curl.exe --path-as-is --http1.1 -sS -i "$basea/dir/%2E%2E/../../Windows/win.ini" | Out-String)
iCheck 'Traversal simples bloqueado com 403' ($travel1a -match '^HTTP/1\.1 403 Forbidden')
iCheck 'Traversal percent-encoded bloqueado com 403' ($travel2a -match '^HTTP/1\.1 403 Forbidden')
iCheck 'Traversal misto bloqueado com 403' ($travel3a -match '^HTTP/1\.1 403 Forbidden')
$spacea = (& curl.exe --path-as-is --http1.1 -sS -i "$basea/nome%20com%20espaco.txt" | Out-String)
iCheck 'Percent-encoding de nome válido' ($spacea -match '^HTTP/1\.1 200 OK')
$utf8Response_a = (& curl.exe --http1.1 -sS -i "$basea/utf8.txt" | Out-String)
$utf8Length_a = [int]([regex]::Match($utf8Response_a, '(?im)^Content-Length: (\d+)').Groups[1].Value)
$utf8DiskLength_a = [IO.File]::ReadAllBytes((Resolve-Path 'src\www\utf8.txt')).Length
iCheck 'Content-Length do texto UTF-8 conta bytes, não caracteres' ($utf8Length_a -eq $utf8DiskLength_a)

$mimeCases_a = @(
    @('index.html', 'text/html'), @('estilo.css', 'text/css'),
    @('app.js', 'text/javascript'), @('dados.json', 'application/json'),
    @('teste.txt', 'text/plain'), @('cachorro.jpg', 'image/jpeg'),
    @('mime.png', 'image/png'), @('mime.pdf', 'application/pdf'),
    @('sem-tipo.zzz', 'application/octet-stream')
)
foreach ($mimeCase_a in $mimeCases_a) {
    $mimeResponse_a = (& curl.exe --http1.1 -sS -I "$basea/$($mimeCase_a[0])" | Out-String)
    $expectedType_a = "Content-Type: $($mimeCase_a[1])"
    iCheck "MIME $($mimeCase_a[0])" ($mimeResponse_a.Contains($expectedType_a))
}

$downloadPath_a = Join-Path $env:TEMP 'labredest1-recebido.jpg'
& curl.exe --http1.1 -sS -o $downloadPath_a "$basea/cachorro.jpg"
$sourceHash_a = (Get-FileHash 'src\www\cachorro.jpg' -Algorithm SHA256).Hash
$downloadHash_a = (Get-FileHash $downloadPath_a -Algorithm SHA256).Hash
iCheck 'Corpo binário JPEG preservado' ($sourceHash_a -eq $downloadHash_a)

$badClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$badStream_a = $badClient_a.GetStream()
$badBytes_a = [Text.Encoding]::ASCII.GetBytes("GET / HTTP/1.1`r`nHost: localhost`r`nBroken-Header`r`n`r`n")
$badStream_a.Write($badBytes_a, 0, $badBytes_a.Length)
$badStream_a.Flush()
$badResponse_a = iReadResponse $badStream_a
iCheck 'Cabeçalho malformado retorna 400' ($badResponse_a.Header -match '^HTTP/1\.1 400 Bad Request')
$badClient_a.Dispose()

$lineClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$lineStream_a = $lineClient_a.GetStream()
$lineBytes_a = [Text.Encoding]::ASCII.GetBytes("GET / HTTP/1.1`r`nBroken-Header`r`n`r`n")
$lineStream_a.Write($lineBytes_a, 0, $lineBytes_a.Length)
$lineStream_a.Flush()
$lineResponse_a = iReadResponse $lineStream_a
iCheck 'Linha de requisição inválida retorna 400' ($lineResponse_a.Header -match '^HTTP/1\.1 400 Bad Request')
$lineClient_a.Dispose()

$fragmentClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$fragmentStream_a = $fragmentClient_a.GetStream()
$fragmentBytes_a = [Text.Encoding]::ASCII.GetBytes("GET /teste.txt HTTP/1.1`r`nHost: localhost`r`nConnection: close`r`n`r`n")
$fragmentStream_a.Write($fragmentBytes_a, 0, 12)
$fragmentStream_a.Flush()
$fragmentStream_a.Write($fragmentBytes_a, 12, $fragmentBytes_a.Length - 12)
$fragmentStream_a.Flush()
$fragmentResponse_a = iReadResponse $fragmentStream_a
iCheck 'Request dividida entre escritas TCP' ($fragmentResponse_a.Header -match '^HTTP/1\.1 200 OK')
$fragmentClient_a.Dispose()

$keepClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$keepStream_a = $keepClient_a.GetStream()
$pipeline_a = [Text.Encoding]::ASCII.GetBytes("GET /teste.txt HTTP/1.1`r`nHost: localhost`r`n`r`nGET /dados.json HTTP/1.1`r`nHost: localhost`r`nConnection: close`r`n`r`n")
$keepStream_a.Write($pipeline_a, 0, $pipeline_a.Length)
$keepStream_a.Flush()
$responseOne_a = iReadResponse $keepStream_a
$responseTwo_a = iReadResponse $keepStream_a
$secondBody_a = [Text.Encoding]::UTF8.GetString($responseTwo_a.Body)
iCheck 'Duas requisições preservadas no mesmo socket' (($responseOne_a.Header -match '^HTTP/1\.1 200 OK') -and ($responseTwo_a.Header -match '^HTTP/1\.1 200 OK') -and ($secondBody_a -match 'mensagem'))
iCheck 'Connection close é refletido e encerra o socket' (($responseTwo_a.Header -match '(?im)^Connection: close\r?$') -and ($keepStream_a.ReadByte() -eq -1))
$keepClient_a.Dispose()

$tenClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$tenStream_a = $tenClient_a.GetStream()
$tenResponses_a = 0
$tenOk_a = $true
for ($requestIndex_a = 0; $requestIndex_a -lt 10; $requestIndex_a++) {
    if ($requestIndex_a -eq 9) {
        $requestText_a = "GET /teste.txt HTTP/1.1`r`nHost: localhost`r`ncOnNeCtIoN: cLoSe`r`n`r`n"
    } else {
        $requestText_a = "GET /teste.txt HTTP/1.1`r`nHost: localhost`r`n`r`n"
    }
    $requestBytes_a = [Text.Encoding]::ASCII.GetBytes($requestText_a)
    $tenStream_a.Write($requestBytes_a, 0, $requestBytes_a.Length)
    $tenStream_a.Flush()
    $tenResponse_a = iReadResponse $tenStream_a
    if ($tenResponse_a.Header -notmatch '^HTTP/1\.1 200 OK') {
        $tenOk_a = $false
    }
    $tenResponses_a++
}
$tenEof_a = $tenStream_a.ReadByte()
iCheck 'Dez requests sequenciais reutilizam o mesmo socket' (($tenResponses_a -eq 10) -and $tenOk_a)
iCheck 'Connection com caixa mista fecha após o décimo response' (($tenResponse_a.Header -match '(?im)^Connection: close\r?$') -and ($tenEof_a -eq -1))
$tenClient_a.Dispose()

$optionsClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$optionsStream_a = $optionsClient_a.GetStream()
$optionsBytes_a = [Text.Encoding]::ASCII.GetBytes("OPTIONS * HTTP/1.1`r`nHost: localhost`r`nConnection: close`r`n`r`n")
$optionsStream_a.Write($optionsBytes_a, 0, $optionsBytes_a.Length)
$optionsStream_a.Flush()
$optionsResponse_a = iReadResponse $optionsStream_a
iCheck 'Método não suportado com request-target * retorna 405' ($optionsResponse_a.Header -match '^HTTP/1\.1 405 Method Not Allowed')
$optionsClient_a.Dispose()

$partialEofClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$partialEofStream_a = $partialEofClient_a.GetStream()
$partialEofBytes_a = [Text.Encoding]::ASCII.GetBytes("GET /teste.txt HTTP/1.1`r`nHost: incomplete")
$partialEofStream_a.Write($partialEofBytes_a, 0, $partialEofBytes_a.Length)
$partialEofStream_a.Flush()
$partialEofClient_a.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)
$partialEofResponse_a = iReadResponse $partialEofStream_a
iCheck 'EOF antes de CRLF CRLF produz 400 e libera a conexão' ($partialEofResponse_a.Header -match '^HTTP/1\.1 400 Bad Request')
$partialEofClient_a.Dispose()

$emptyEofClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$emptyEofClient_a.ReceiveTimeout = 3000
$emptyEofStream_a = $emptyEofClient_a.GetStream()
$emptyEofClient_a.Client.Shutdown([System.Net.Sockets.SocketShutdown]::Send)
try { $emptyEofByte_a = $emptyEofStream_a.ReadByte() } catch [System.IO.IOException] { $emptyEofByte_a = -1 }
iCheck 'EOF sem requisição fecha sem resposta' ($emptyEofByte_a -eq -1)
$emptyEofClient_a.Dispose()
$afterEofResponse_a = (& curl.exe --max-time 2 --http1.1 -sS -i "$basea/teste.txt" | Out-String)
iCheck 'Servidor continua atendendo após EOF prematuro' ($afterEofResponse_a -match '^HTTP/1\.1 200 OK')

$slowClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$slowStream_a = $slowClient_a.GetStream()
$partial_a = [Text.Encoding]::ASCII.GetBytes('GET /')
$slowStream_a.Write($partial_a, 0, $partial_a.Length)
$otherResponse_a = (& curl.exe --max-time 2 --http1.1 -sS -i "$basea/teste.txt" | Out-String)
iCheck 'Cliente lento não bloqueia outra conexão' ($otherResponse_a -match '^HTTP/1\.1 200 OK')
$slowClient_a.Dispose()

$timeoutClient_a = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 8080)
$timeoutClient_a.ReceiveTimeout = 8000
$timeoutStream_a = $timeoutClient_a.GetStream()
$timeoutRequest_a = [Text.Encoding]::ASCII.GetBytes("GET /teste.txt HTTP/1.1`r`nHost: localhost`r`n`r`n")
$timeoutStream_a.Write($timeoutRequest_a, 0, $timeoutRequest_a.Length)
$timeoutStream_a.Flush()
$timeoutResponse_a = iReadResponse $timeoutStream_a
try {
    $timeoutEnd_a = $timeoutStream_a.ReadByte()
} catch [System.IO.IOException] {
    $timeoutEnd_a = -1
}
iCheck 'Timeout ocioso fecha socket depois da resposta' (($timeoutResponse_a.Header -match '^HTTP/1\.1 200 OK') -and ($timeoutEnd_a -eq -1))
$timeoutClient_a.Dispose()

Write-Output 'TODOS OS TESTES LOCAIS PASSARAM.'
