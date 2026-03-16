import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/user_profile_model.dart';

class UserService {
  Map<String, String> _headers(String token) => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<UserProfileModel> getProfile(String token) async {
    final response = await http.get(
      Uri.parse(ApiConstants.userMe),
      headers: _headers(token),
    );
    if (response.statusCode == 200) {
      return UserProfileModel.fromJson(jsonDecode(response.body));
    }
    throw Exception('Erro ao carregar perfil');
  }

  Future<UserProfileModel> updateProfile(
      String token, String nome, String? senha, String? email) async {
    final body = <String, String>{'nome': nome};
    if (senha != null && senha.isNotEmpty) body['senha'] = senha;
    if (email != null && email.isNotEmpty) body['email'] = email;

    final response = await http.put(
      Uri.parse(ApiConstants.userMe),
      headers: _headers(token),
      body: jsonEncode(body),
    );
    if (response.statusCode == 200) {
      return UserProfileModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
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
