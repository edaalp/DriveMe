import 'package:flutter/material.dart';
import 'screens/driver_home.dart';

void main() {
  runApp(const DriveMeApp());
}

class DriveMeApp extends StatelessWidget {
  const DriveMeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'DriveMe Demo',
      theme: ThemeData(useMaterial3: true),
      home: const DriverHome(),
    );
  }
}
