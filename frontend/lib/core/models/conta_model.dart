class ContaModel {
  final int id;
  final String nome;
  final double saldo;
  final String? numeroConta;
  final String? agencia;

  ContaModel({
    required this.id,
    required this.nome,
    required this.saldo,
    this.numeroConta,
    this.agencia,
  });

  factory ContaModel.fromJson(Map<String, dynamic> json) => ContaModel(
        id: json['id'] as int,
        nome: json['nome'] as String,
        saldo: (json['saldo'] as num).toDouble(),
        numeroConta: json['numeroConta'] as String?,
        agencia: json['agencia'] as String?,
      );
}
