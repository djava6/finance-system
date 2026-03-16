class CategoriaModel {
  final int id;
  final String nome;

  CategoriaModel({required this.id, required this.nome});

  factory CategoriaModel.fromJson(Map<String, dynamic> json) => CategoriaModel(
        id: json['id'] as int,
        nome: json['nome'] as String,
      );
}
