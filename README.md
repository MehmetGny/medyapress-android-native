# MedyaPress Android Native v1.5

Bu paket GitHub Actions ile APK üretmek için hazırlanmıştır.

## ZIP içeriği

- app/
- build.gradle
- settings.gradle
- README.md
- .github/workflows/android.yml

## GitHub ile APK üretme

1. Bu klasörün içindeki dosyaları GitHub repo köküne yükleyin.
2. Actions sekmesine girin.
3. Android CI workflow çalışınca APK artifact olarak oluşur.

Not: Bu paket `gradlew` zorunlu kılmaz. Workflow, GitHub üzerinde Gradle kurup `gradle assembleDebug` komutu çalıştırır.
