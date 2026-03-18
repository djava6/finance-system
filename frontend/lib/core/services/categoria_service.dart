import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/categoria_model.dart';
import 'api_client.dart';

class CategoriaService {
  final _client = ApiClient();

  Future<List<CategoriaModel>> listar() async {
    final response = await _client.get(Uri.parse(ApiConstants.categories));
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => CategoriaModel.fromJson(e)).toList();
    }
    throw Exception('Erro ao carregar categorias');
  }

  Future<CategoriaModel> criar(String nome) async {
    final response = await _client.post(
      Uri.parse(ApiConstants.categories),
      body: jsonEncode({'nome': nome}),
    );
    if (response.statusCode == 201) {
      return CategoriaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<CategoriaModel> renomear(int id, String nome) async {
    final response = await _client.put(
      Uri.parse('${ApiConstants.categories}/$id'),
      body: jsonEncode({'nome': nome}),
    );
    if (response.statusCode == 200) {
      return CategoriaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(int id) async {
    final response =
        await _client.delete(Uri.parse('${ApiConstants.categories}/$id'));
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
