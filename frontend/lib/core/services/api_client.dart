import 'dart:async';
import 'dart:io';
import 'package:http/http.dart' as http;

class ApiClient {
  static const _timeout = Duration(seconds: 30);
  static const _retryDelay = Duration(seconds: 2);
  static const _maxRetries = 2;

  final String token;

  ApiClient(this.token);

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<http.Response> get(Uri uri) => _withRetry(() =>
      http.get(uri, headers: _headers).timeout(_timeout));

  Future<http.Response> post(Uri uri, {required String body}) =>
      http.post(uri, headers: _headers, body: body).timeout(_timeout);

  Future<http.Response> put(Uri uri, {required String body}) =>
      http.put(uri, headers: _headers, body: body).timeout(_timeout);

  Future<http.Response> delete(Uri uri) =>
      http.delete(uri, headers: _headers).timeout(_timeout);

  Future<http.Response> _withRetry(
    Future<http.Response> Function() call,
  ) async {
    int attempt = 0;
    while (true) {
      try {
        return await call();
      } on SocketException {
        if (++attempt > _maxRetries) rethrow;
        await Future.delayed(_retryDelay);
      } on TimeoutException {
        if (++attempt > _maxRetries) rethrow;
        await Future.delayed(_retryDelay);
      }
    }
  }
}
