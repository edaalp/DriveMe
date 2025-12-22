import 'package:flutter/material.dart';
import '../ws/ws_client.dart';

class DriverHome extends StatefulWidget {
  const DriverHome({super.key});

  @override
  State<DriverHome> createState() => _DriverHomeState();
}

class _DriverHomeState extends State<DriverHome> {
  final events = <Map<String, dynamic>>[];
  late final DriveMeWsClient ws;

  @override
  void initState() {
    super.initState();

    // Android emulator uses 10.0.2.2 to reach host machine localhost
    ws = DriveMeWsClient('ws://10.0.2.2:8080/ws');

    ws.connect(onConnected: () {
      ws.subscribeToRequests((e) {
        setState(() => events.insert(0, e));
      });
    });
  }

  @override
  void dispose() {
    ws.disconnect();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Driver - Live Requests')),
      body: ListView.builder(
        itemCount: events.length,
        itemBuilder: (_, i) {
          final e = events[i];
          return ListTile(
            title: Text('Request #${e["requestId"]}'),
            subtitle: Text('Pickup: (${e["pickupLat"]}, ${e["pickupLng"]})'),
          );
        },
      ),
    );
  }
}
