class ContaModel {
  final int id;
  final String nome;
  final double saldo;

  ContaModel({required this.id, required this.nome, required this.saldo});

  factory ContaModel.fromJson(Map<String, dynamic> json) => ContaModel(
        id: json['id'] as int,
        nome: json['nome'] as String,
        saldo: (json['saldo'] as num).toDouble(),
      );
}
