import 'dart:async';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import '../constants/api_constants.dart';

class WebSocketService {
  StompClient? _client;
  final _controller = StreamController<String>.broadcast();
  bool _connected = false;

  Stream<String> get updates => _controller.stream;

  Future<void> connect(String usuarioId) async {
    if (_connected) return;

    final token = await FirebaseAuth.instance.currentUser?.getIdToken();
    if (token == null) return;

    _client = StompClient(
      config: StompConfig(
        url: ApiConstants.wsUrl,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        onConnect: (frame) {
          _connected = true;
          _client?.subscribe(
            destination: '/topic/transacoes/$usuarioId',
            callback: (_) => _controller.add('updated'),
          );
        },
        onDisconnect: (_) => _connected = false,
        onWebSocketError: (_) => _connected = false,
        reconnectDelay: const Duration(seconds: 5),
      ),
    );
    _client!.activate();
  }

  void disconnect() {
    _client?.deactivate();
    _connected = false;
  }

  void dispose() {
    disconnect();
    _controller.close();
  }
}
