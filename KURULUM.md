# MelWeather — Hava Durumu + Gökyüzü Paylaşım Uygulaması

Firebase'e bağlı, uçak takibi **olmayan**, hava durumu + fotoğraf paylaşımlı sürüm.
Tasarım, `kullan.pdf` tasarım panosundaki MelWeather konseptine göre yapıldı: saatlik tahmin
şeridi, 2 sütunlu "Shared Moments" galerisi ve kamera/galeri seçenekleri açılan speed-dial
paylaşım butonu.

## 1. Firestore güvenlik kurallarını yapıştır (önemli!)
Firebase Console → **Firestore Database** → üstte **Rules** sekmesi → içindekini tamamen sil,
bu projedeki `firestore.rules` dosyasının içeriğini yapıştır → **Publish**.

Bu olmadan uygulama paylaşım okuyamaz/yazamaz (izin hatası alırsın).

## 2. Android Studio'da aç
1. Android Studio → **File → Open** → `Masaüstü\AndroidStudio Apps\HavaDurumu` klasörünü seç.
2. "Trust Project?" → **Trust Project**.
3. Gradle senkronizasyonu çalışır (internet gerekir, birkaç dakika sürebilir).
4. Üstte cihaz olarak **Medium Phone API 37** seçili olsun → yeşil ▶ **Run**.

## 3. Uygulamayı kullanma
1. Açılışta **giriş ekranı** çıkar. Henüz hesabın yoksa: kullanıcı adı + şifre (en az 6 karakter)
   gir → **"Hesabın yok mu? Kayıt ol"** butonuna bas.
2. Giriş yapınca ana ekran açılır: konum izni ister → **İzin ver**.
3. Üstte bulunduğun yerin hava durumu, altında 5 günlük tahmin, en altta **gökyüzü paylaşımları akışı**.
4. Sağ alttaki **"📷 Gökyüzünü paylaş"** butonuna basarak kamera ile çek ya da galeriden seç → **Paylaş**.
   Fotoğraf otomatik olarak şehir adı, kullanıcı adı, saat ve o anki hava durumu etiketiyle feed'e eklenir.
5. Başka bir şehrin hava durumuna bakmak için üstteki arama kutusunu kullan.

### Emülatörde konum ayarlama
Emülatörün varsayılan konumu ABD'dir. Kendi şehrini ayarlamak için: emülatör yanındaki
**⋮ (Extended Controls) → Location** → haritada şehrini ara → **Set Location**, sonra
uygulamada **📍 Konumum**'a bas.

## 4. Ana ekran widget'ını ekleme
Önce uygulamayı en az bir kere açıp hava durumunun yüklenmesini bekle (widget, uygulamanın
en son gördüğü hava durumunu gösterir).

**Emülatörde:**
1. Ana ekrana dön (▢ veya ⌂ tuşu).
2. Boş bir alana **basılı tut** → açılan menüden **Widgets**'a dokun.
3. Listede **Hava Durumu** uygulamasını bul, widget'ına basılı tutup ana ekrana sürükle.
4. Açılan ekranda **Kamera** veya **Galeri**'den bir arka plan fotoğrafı seç (istemiyorsan
   **"Fotoğrafsız, varsayılan görünümle ekle"**'ye bas) → **Widget'ı Ekle**.
5. Widget ana ekranda görünür; üstüne dokunursan uygulama açılır.

Gerçek telefonda da aynı adımlar geçerli (üretici arayüzüne göre "Widget ekle" menüsü
değişebilir, genelde ana ekranda basılı tutup "Widgets" seçilir).

## 5. Hocanın istediği doğrulama ekran görüntüleri
1. Giriş/kayıt ekranı.
2. Ana ekran: konumuna göre hava durumu + 5 günlük tahmin.
3. Aynı anda **open-meteo.com** veya **mgm.gov.tr**'de aynı şehrin hava durumu → sıcaklık/nem/rüzgar eşleşiyor.
4. Başka bir şehir arama sonucu.
5. Bir gökyüzü fotoğrafı paylaşma akışı ve feed'de göründüğü hali (şehir/kullanıcı/saat etiketiyle).
6. Firebase Console'da **Firestore Database → posts** koleksiyonunda o paylaşımın veri olarak
   kaydedildiğini gösteren ekran görüntüsü (verinin gerçekten sunucuda tutulduğunu kanıtlar).
7. Ana ekrana eklenmiş hava durumu widget'ının ekran görüntüsü.

## Proje yapısı
- `LoginActivity.kt` → kullanıcı adı/şifre ile giriş-kayıt (Firebase Authentication)
- `MainActivity.kt` → konum, hava durumu, şehir arama, feed listesi
- `UploadActivity.kt` → kamera/galeri seçimi, fotoğraf sıkıştırma, Firestore'a kaydetme
- `FeedAdapter.kt` → RecyclerView: 1 başlık (hava durumu) + paylaşım kartları
- `Post.kt` → Firestore veri modeli
- `WeatherWidgetProvider.kt` → ana ekran widget'ının verisini çizer
- `WidgetConfigActivity.kt` → widget eklenirken arka plan fotoğrafı seçtirir
- `firestore.rules` → Firebase Console'a yapıştırılacak güvenlik kuralları

## Kullanılan servisler
- Hava durumu: Open-Meteo — api.open-meteo.com (API anahtarı gerekmez)
- Şehir arama: Open-Meteo Geocoding
- Giriş + veri + fotoğraf: Firebase Authentication + Firestore (ücretsiz Spark plan)
