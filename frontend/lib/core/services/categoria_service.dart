import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/categoria_model.dart';

class CategoriaService {
  Map<String, String> _headers(String token) => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<List<CategoriaModel>> listar(String token) async {
    final response = await http.get(
      Uri.parse(ApiConstants.categories),
      headers: _headers(token),
    );
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => CategoriaModel.fromJson(e)).toList();
    }
    throw Exception('Erro ao carregar categorias');
  }

  Future<CategoriaModel> criar(String token, String nome) async {
    final response = await http.post(
      Uri.parse(ApiConstants.categories),
      headers: _headers(token),
      body: jsonEncode({'nome': nome}),
    );
    if (response.statusCode == 201) {
      return CategoriaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<CategoriaModel> renomear(String token, int id, String nome) async {
    final response = await http.put(
      Uri.parse('${ApiConstants.categories}/$id'),
      headers: _headers(token),
      body: jsonEncode({'nome': nome}),
    );
    if (response.statusCode == 200) {
      return CategoriaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(String token, int id) async {
    final response = await http.delete(
      Uri.parse('${ApiConstants.categories}/$id'),
      headers: _headers(token),
    );
    if (response.statusCode != 204) {
      throw Exception(_extractError(response.body));
    }
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
