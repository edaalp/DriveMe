import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';

class DriveMeWsClient {
  final String wsUrl; // e.g. ws://10.0.2.2:8080/ws for Android emulator
  StompClient? _client;

  DriveMeWsClient(this.wsUrl);

  void connect({required void Function() onConnected}) {
    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (_) => onConnected(),
        onWebSocketError: (dynamic err) => print('WS error: $err'),
        onStompError: (frame) => print('STOMP error: ${frame.body}'),
        onDisconnect: (_) => print('WS disconnected'),
        reconnectDelay: const Duration(seconds: 5),
      ),
    );
    _client!.activate();
  }

  StompUnsubscribe subscribeToRequests(void Function(Map<String, dynamic>) onEvent) {
    final sub = _client!.subscribe(
      destination: '/topic/requests',
      callback: (frame) {
        if (frame.body == null) return;
        final data = jsonDecode(frame.body!) as Map<String, dynamic>;
        onEvent(data);
      },
    );
    return sub;
  }

  void disconnect() {
    _client?.deactivate();
  }
}
