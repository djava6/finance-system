import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/user_profile_model.dart';
import 'api_client.dart';

class UserService {
  final _client = ApiClient();

  Future<UserProfileModel> getProfile() async {
    final response = await _client.get(Uri.parse(ApiConstants.userMe));
    if (response.statusCode == 200) {
      return UserProfileModel.fromJson(jsonDecode(response.body));
    }
    throw Exception('Erro ao carregar perfil');
  }

  Future<UserProfileModel> updateProfile(String nome, String? email) async {
    final body = <String, String>{'nome': nome};
    if (email != null && email.isNotEmpty) body['email'] = email;

    final response = await _client.put(
      Uri.parse(ApiConstants.userMe),
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
