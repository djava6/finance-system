import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:google_sign_in/google_sign_in.dart';
import '../constants/api_constants.dart';
import '../models/auth_response.dart';

class AuthService {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    serverClientId: '845670274575-0v7qu9okla1spq64ksk6ahb91faed40c.apps.googleusercontent.com',
  );

  Future<AuthResponse> loginWithGoogle() async {
    final googleUser = await _googleSignIn.signIn();
    if (googleUser == null) throw Exception('Login cancelado.');

    final googleAuth = await googleUser.authentication;
    final idToken = googleAuth.idToken;
    if (idToken == null) throw Exception('Não foi possível obter o token do Google.');

    final response = await http.post(
      Uri.parse(ApiConstants.googleAuth),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'idToken': idToken}),
    );

    if (response.statusCode == 200) {
      return AuthResponse.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<AuthResponse> login(String email, String senha) async {
    final response = await http.post(
      Uri.parse(ApiConstants.login),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'senha': senha}),
    );

    if (response.statusCode == 200) {
      return AuthResponse.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<AuthResponse> register(
      String nome, String email, String senha) async {
    final response = await http.post(
      Uri.parse(ApiConstants.register),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'nome': nome, 'email': email, 'senha': senha}),
    );

    if (response.statusCode == 201) {
      return AuthResponse.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<AuthResponse> refreshToken(String refreshToken) async {
    final response = await http.post(
      Uri.parse(ApiConstants.refresh),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refreshToken}),
    );

    if (response.statusCode == 200) {
      return AuthResponse.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> logout(String token) async {
    await http.post(
      Uri.parse(ApiConstants.logout),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );
  }

  String _extractError(String body) {
    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      return json['erro'] as String? ?? 'Erro desconhecido';
    } catch (_) {
      return 'Erro ao conectar com o servidor';
    }
  }
}