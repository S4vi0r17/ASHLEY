# 🤖 Configuración de Gemini AI para Mejora de Mensajes

## ¿Qué es esto?

Tu app ahora tiene un botón con IA (✨) que mejora automáticamente los mensajes del chat:
- Corrige ortografía y gramática
- Hace los mensajes más amigables
- Mejora la coherencia del texto
- Mantiene el tono casual apropiado para chat

## 🚀 Cómo Obtener tu API Key GRATIS

### Paso 1: Ir a Google AI Studio
1. Abre tu navegador y ve a: **https://aistudio.google.com/app/apikey**
2. Inicia sesión con tu cuenta de Google

### Paso 2: Crear una API Key
1. Haz clic en **"Create API Key"** o **"Crear clave de API"**
2. Selecciona un proyecto de Google Cloud (o crea uno nuevo)
3. Copia la API Key que se genera (es una cadena larga de letras y números)

⚠️ **IMPORTANTE**: Guarda esta key en un lugar seguro. No la compartas públicamente.

### Paso 3: Configurar en tu App
1. Abre el archivo: `app/src/main/java/com/grupo2/ashley/chat/ai/GeminiAIService.kt`
2. Busca la línea que dice:
   ```kotlin
   private val apiKey = "TU_API_KEY_AQUI"
   ```
3. Reemplaza `"TU_API_KEY_AQUI"` con tu API Key real:
   ```kotlin
   private val apiKey = "AIzaSyABC123def456..." // Tu key real aquí
   ```
4. Guarda el archivo

### Paso 4: ¡Listo!
- Ejecuta la app
- Abre cualquier chat
- Escribe un mensaje
- Toca el botón ✨ (estrellitas mágicas)
- ¡El texto se mejorará automáticamente!

## 📱 Cómo Funciona

1. **Escribes un mensaje**: Por ejemplo: "ola komo tas el prducto todavia esta"
2. **Tocas el botón ✨**: El botón con las estrellitas mágicas
3. **La IA lo mejora**: "Hola, ¿cómo estás? ¿El producto todavía está disponible?"
4. **Envías el mensaje mejorado**: Presiona enviar y listo

## 🎨 Características del Botón

- **Solo aparece** cuando hay texto escrito
- **Animación de carga** mientras la IA trabaja (loading spinner)
- **Color especial**: Fondo con color terciario (cyan/turquesa)
- **Icono "AutoAwesome"**: Representa la magia de la IA ✨

## 💰 Límites Gratuitos de Gemini

Gemini API tiene un **nivel gratuito generoso**:
- **15 solicitudes por minuto**
- **1,500 solicitudes por día**
- **1 millón de tokens gratis al mes**

Para una app de chat, esto es más que suficiente. Cada mejora de mensaje usa ~100-200 tokens.

## 🔒 Seguridad

**NUNCA** subas tu API key a GitHub o repositorios públicos. Para producción, considera:
- Usar Firebase Remote Config para almacenar la key
- Crear un backend que maneje las llamadas a Gemini
- Usar variables de entorno

## ⚙️ Personalización

Puedes modificar cómo mejora los mensajes editando el prompt en `GeminiAIService.kt`:

```kotlin
val prompt = """
    Eres un asistente que ayuda a mejorar mensajes de chat...

    Tu tarea es:
    1. Corregir la ortografía y gramática
    2. Hacer el mensaje más claro y amigable
    // ... puedes agregar más instrucciones aquí
""".trimIndent()
```

## 🐛 Solución de Problemas

### "API Key no configurada"
- Verifica que reemplazaste `"TU_API_KEY_AQUI"` con tu key real
- Asegúrate de que la key está entre comillas

### "Error: API_KEY_INVALID"
- Tu API key puede estar incorrecta
- Genera una nueva key en Google AI Studio

### "Error: RATE_LIMIT_EXCEEDED"
- Has excedido el límite gratuito (15 solicitudes/minuto)
- Espera un minuto y vuelve a intentar

### El botón no hace nada
- Verifica que tienes internet
- Revisa los logs de Android Studio para ver el error específico

## 📚 Documentación Oficial

- **Gemini API**: https://ai.google.dev/
- **Google AI Studio**: https://aistudio.google.com/
- **Precios**: https://ai.google.dev/pricing

---

¡Disfruta de tu función de IA en el chat! ✨🚀
