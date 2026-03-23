/// JSON fixtures that mirror real API responses.

const dashboardJson = '''
{
  "saldo": 1500.00,
  "totalReceitas": 3000.00,
  "totalDespesas": 1500.00,
  "contas": [
    {"id": 1, "nome": "Nubank", "saldo": 1500.00, "banco": null, "agencia": null, "numeroConta": null}
  ],
  "ultimasTransacoes": [
    {
      "id": 1, "descricao": "Salário", "valor": 3000.00, "tipo": "RECEITA",
      "data": "2026-03-01", "recorrente": false
    },
    {
      "id": 2, "descricao": "Supermercado", "valor": 500.00, "tipo": "DESPESA",
      "data": "2026-03-05", "categoria": "Alimentação", "categoriaId": 1, "recorrente": false
    }
  ],
  "despesasPorCategoria": [
    {"categoria": "Alimentação", "total": 800.00},
    {"categoria": "Transporte", "total": 700.00}
  ],
  "evolucaoMensal": [
    {"ano": 2026, "mes": 2, "totalReceitas": 2800.00, "totalDespesas": 1200.00},
    {"ano": 2026, "mes": 3, "totalReceitas": 3000.00, "totalDespesas": 1500.00}
  ]
}
''';

const transacoesJson = '''
{
  "content": [
    {
      "id": 1, "descricao": "Salário", "valor": 3000.00, "tipo": "RECEITA",
      "data": "2026-03-01", "recorrente": false
    },
    {
      "id": 2, "descricao": "Supermercado", "valor": 500.00, "tipo": "DESPESA",
      "data": "2026-03-05", "categoria": "Alimentação", "categoriaId": 1, "recorrente": false
    },
    {
      "id": 3, "descricao": "Aluguel", "valor": 1000.00, "tipo": "DESPESA",
      "data": "2026-03-10", "categoria": "Moradia", "categoriaId": 2,
      "recorrente": true, "frequencia": "MENSAL"
    }
  ],
  "page": 0, "size": 20, "totalElements": 3, "totalPages": 1
}
''';

const categoriasJson = '''
[
  {"id": 1, "nome": "Alimentação"},
  {"id": 2, "nome": "Moradia"},
  {"id": 3, "nome": "Transporte"},
  {"id": 4, "nome": "Lazer"},
  {"id": 5, "nome": "Patrimônio"}
]
''';

const contasJson = '''
[
  {"id": 1, "nome": "Nubank", "saldo": 1500.00, "banco": null, "agencia": null, "numeroConta": null}
]
''';

const novaTransacaoJson = '''
{
  "id": 10, "descricao": "Teste E2E", "valor": 100.00, "tipo": "DESPESA",
  "data": "2026-03-22", "recorrente": false
}
''';

const orcamentosJson = '''
[
  {"id": 1, "categoriaId": 1, "categoria": "Alimentação", "valorLimite": 1000.00, "valorGasto": 800.00, "percentual": 80.0, "mes": 3, "ano": 2026},
  {"id": 2, "categoriaId": 3, "categoria": "Transporte", "valorLimite": 500.00, "valorGasto": 200.00, "percentual": 40.0, "mes": 3, "ano": 2026}
]
''';

const metasJson = '''
[
  {"id": 1, "nome": "Reserva de emergência", "valorAlvo": 10000.00, "valorAtual": 3000.00, "percentual": 30.0, "concluida": false},
  {"id": 2, "nome": "Viagem", "valorAlvo": 5000.00, "valorAtual": 5000.00, "percentual": 100.0, "concluida": true}
]
''';
