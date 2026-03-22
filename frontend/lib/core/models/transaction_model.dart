class TransactionModel {
  final int id;
  final String descricao;
  final double valor;
  final String tipo; // RECEITA | DESPESA
  final DateTime data;
  final int? categoriaId;
  final String? categoria;
  final int? contaId;
  final String? conta;
  final bool recorrente;
  final String? frequencia; // SEMANAL | MENSAL | ANUAL
  final DateTime? proximaOcorrencia;

  TransactionModel({
    required this.id,
    required this.descricao,
    required this.valor,
    required this.tipo,
    required this.data,
    this.categoriaId,
    this.categoria,
    this.contaId,
    this.conta,
    this.recorrente = false,
    this.frequencia,
    this.proximaOcorrencia,
  });

  bool get isReceita => tipo == 'RECEITA';

  factory TransactionModel.fromJson(Map<String, dynamic> json) =>
      TransactionModel(
        id: json['id'] as int,
        descricao: json['descricao'] as String,
        valor: (json['valor'] as num).toDouble(),
        tipo: json['tipo'] as String,
        data: DateTime.parse(json['data'] as String),
        categoriaId: json['categoriaId'] as int?,
        categoria: json['categoria'] as String?,
        contaId: json['contaId'] as int?,
        conta: json['conta'] as String?,
        recorrente: json['recorrente'] as bool? ?? false,
        frequencia: json['frequencia'] as String?,
        proximaOcorrencia: json['proximaOcorrencia'] != null
            ? DateTime.parse(json['proximaOcorrencia'] as String)
            : null,
      );
}