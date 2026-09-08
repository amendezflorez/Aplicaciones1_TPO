package com.example.rondaapp;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Punto de arranque del contenedor de Hilt. No tiene lógica propia: la anotación
 * es lo que genera el componente que después inyecta en Activity y Fragments.
 */
@HiltAndroidApp
public class RondaApp extends Application {
}
