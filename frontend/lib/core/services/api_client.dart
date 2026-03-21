import 'dart:async';
import 'dart:io';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:http/http.dart' as http;

class ApiClient {
  static const _timeout = Duration(seconds: 30);
  static const _retryDelay = Duration(seconds: 2);
  static const _maxRetries = 2;

  /// Called when a 401 persists after token refresh — use to trigger logout.
  static void Function()? onUnauthorized;

  Future<Map<String, String>> _headers({bool forceRefresh = false}) async {
    final token = await FirebaseAuth.instance.currentUser
        ?.getIdToken(forceRefresh);
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<http.Response> get(Uri uri) =>
      _execute((h) => http.get(uri, headers: h));

  Future<http.Response> post(Uri uri, {required String body}) =>
      _execute((h) => http.post(uri, headers: h, body: body));

  Future<http.Response> put(Uri uri, {required String body}) =>
      _execute((h) => http.put(uri, headers: h, body: body));

  Future<http.Response> patch(Uri uri, {required String body}) =>
      _execute((h) => http.patch(uri, headers: h, body: body));

  Future<http.Response> delete(Uri uri) =>
      _execute((h) => http.delete(uri, headers: h));

  Future<http.Response> postMultipart(
    Uri uri, {
    required List<int> fileBytes,
    required String fileName,
    required String fieldName,
  }) async {
    final token = await FirebaseAuth.instance.currentUser?.getIdToken(false);
    final request = http.MultipartRequest('POST', uri);
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    request.files.add(http.MultipartFile.fromBytes(
      fieldName,
      fileBytes,
      filename: fileName,
    ));
    final streamed = await request.send().timeout(_timeout);
    return http.Response.fromStream(streamed);
  }

  Future<http.Response> _execute(
    Future<http.Response> Function(Map<String, String> headers) fn,
  ) async {
    var headers = await _headers();
    var response = await _withRetry(() => fn(headers).timeout(_timeout));
    if (response.statusCode == 401) {
      headers = await _headers(forceRefresh: true);
      response = await _withRetry(() => fn(headers).timeout(_timeout));
      if (response.statusCode == 401) {
        onUnauthorized?.call();
      }
    }
    return response;
  }

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
