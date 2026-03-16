class TransactionModel {
  final int id;
  final String descricao;
  final double valor;
  final String tipo; // RECEITA | DESPESA
  final DateTime data;
  final String? categoria;
  final String? conta;

  TransactionModel({
    required this.id,
    required this.descricao,
    required this.valor,
    required this.tipo,
    required this.data,
    this.categoria,
    this.conta,
  });

  bool get isReceita => tipo == 'RECEITA';

  factory TransactionModel.fromJson(Map<String, dynamic> json) =>
      TransactionModel(
        id: json['id'] as int,
        descricao: json['descricao'] as String,
        valor: (json['valor'] as num).toDouble(),
        tipo: json['tipo'] as String,
        data: DateTime.parse(json['data'] as String),
        categoria: json['categoria'] as String?,
        conta: json['conta'] as String?,
      );
}