fetch("dados.json")
  .then((response) => response.json())
  .then((dados) => {
    document.querySelector("#status").textContent = dados.mensagem;
  })
  .catch(() => {
    document.querySelector("#status").textContent = "Falha ao carregar dados.json";
  });
