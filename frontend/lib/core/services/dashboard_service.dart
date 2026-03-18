import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/dashboard_model.dart';
import 'api_client.dart';

class DashboardService {
  Future<DashboardModel> getDashboard(String token) async {
    final client = ApiClient(token);
    final response = await client.get(Uri.parse(ApiConstants.dashboard));
    if (response.statusCode == 200) {
      return DashboardModel.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception('Erro ao carregar dashboard (HTTP ${response.statusCode})');
  }
}
