import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() {
  runApp(const TikapubApp());
}

class TikapubApp extends StatelessWidget {
  const TikapubApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Tikapub',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6A1B9A)),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
