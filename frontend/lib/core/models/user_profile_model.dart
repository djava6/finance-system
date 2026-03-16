class UserProfileModel {
  final int id;
  final String nome;
  final String email;

  UserProfileModel({required this.id, required this.nome, required this.email});

  factory UserProfileModel.fromJson(Map<String, dynamic> json) =>
      UserProfileModel(
        id: json['id'] as int,
        nome: json['nome'] as String,
        email: json['email'] as String,
      );
}
