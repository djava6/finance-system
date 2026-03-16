class AuthResponse {
  final String token;
  final String refreshToken;
  final String nome;
  final String email;

  AuthResponse({
    required this.token,
    required this.refreshToken,
    required this.nome,
    required this.email,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) => AuthResponse(
        token: json['token'] as String,
        refreshToken: json['refreshToken'] as String? ?? '',
        nome: json['nome'] as String,
        email: json['email'] as String,
      );
}