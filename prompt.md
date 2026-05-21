Sen üst düzey, profesyonel bir Minecraft Java Plugin Geliştiricisisin. Paper API 1.21+ sürümü için son derece optimize edilmiş, hafif ve sunucuda kesinlikle lag yaratmayacak "GunlukOduller" isimli bir günlük ödül eklentisi yazmanı istiyorum. 

Aşağıdaki tüm gereksinimleri eksiksiz bir şekilde yerine getirmeli ve temiz, açıklayıcı kod (Clean Code) yazmalısın.

### 1. Genel Bilgiler ve Temel Ayarlar
- **Plugin Adı:** GunlukOduller
- **Yapımcı:** wulnrydev
- **Sürüm:** 1.21+
- **Prefix:** `&#15FF08&lSurvival &8▸ `
- **Renk Paleti:** Ana metinler `&7`, birincil vurgular `&#00D420`, ikincil vurgular `&#ffd500` olacak. Hex formatları modern MiniMessage veya Bungee API (ChatColor) formatıyla tam uyumlu çalışmalı.
- **Optimizasyon:** Veri kaydetme/okuma işlemleri (YAML veya SQLite) kesinlikle ASYNC (ana thread'i meşgul etmeyecek şekilde) yapılmalı. Oyuncu verileri cache'de tutulmalı, sunucu kapanırken veya periyodik olarak kaydedilmelidir.

### 2. Komutlar ve Menü Mantığı
- **Komut:** `/günlüködül` (Bu komutu kullanmak için hiçbir yetkiye gerek olmayacak).
- **Menü Başlığı:** `&0Günlük Ödül`
- **Menü Boyutu:** 36 Slot (4 Satır).
- **Asistan Butonu:** Menünün en alt orta kısmında (Slot 31) bir "Ok" (ARROW) eşyası olacak. Adı "Asistan Menüsüne Dön" olacak. Oyuncu bu oka tıkladığında menü kapanacak ve oyuncu otomatik olarak sohbete `asistan` komutunu girmiş gibi çalışacak (örn: `player.performCommand("asistan")`).

### 3. Ödül Kategorileri ve Yetkiler
- Config üzerinden tamamen ayarlanabilir 7 farklı ödül kategorisi olacak: 
  1. Oyuncu
  2. VIP
  3. VIP+
  4. UVIP
  5. UVIP+
  6. MossVIP
  7. MossVIP+
- Her bir ödül için config dosyasında özel bir permission (örn: `gunlukodul.vip`, `gunlukodul.mossvip`) belirlenecek.
- Her ödül alındığında config'de belirlenen komutlar konsol üzerinden çalıştırılacak (örn: `eco give %player% 1000`).
- Ödüller 24 saatte bir (veya her gün gece 00:00'da yenilenecek şekilde, configden seçilebilir) SADECE 1 KERE alınabilecek.

### 4. Menü İçi Görünüm, Eşyalar ve Diziliş
- Menüdeki eşyaların dizilişi ortalanmış ve şık bir şekilde şu mantıkla sıralanmalı:
  - (Varsa ilk satırda Oyuncu ödülü)
  - Orta kısımlarda sırasıyla: Oyuncu, VIP, VIP+, UVIP, UVIP+, MossVIP, MossVIP+
- **Durum (Status) Mantığı:** Eşyaların Lore (açıklama) kısmında ödülün durumu dinamik olarak yazmalı. 
  - Alınabilir durumda ise: `Durum: &#00D420Alınabilir`
  - Alınamaz/Zaten alınmış/Yetki yok ise: `Durum: &cAlınamaz`
- **Material Değişimi:**
  - Eğer oyuncunun yetkisi yoksa, o günkü ödülü çoktan almışsa veya ödül şu an alınamaz durumdaysa menüdeki icon: `TNT_MINECART` (TNT'li Vagon) olmalı.
  - Eğer oyuncu ödülü şu an **alabiliyorsa** icon: `CHEST_MINECART` (Sandıklı Vagon) olmalı. (Saatli vagon görünümü istenirse CustomModelData config'e eklenebilmeli).

### 5. Etkileşimler, Sesler ve Mesajlar
- Menüdeki hiçbir eşya oyuncunun envanterine alınamamalı (InventoryClickEvent iptal edilmeli).
- **Ses Desteği:** 
  - Ödül başarıyla alındığında tatmin edici bir başarı sesi (örn: `ENTITY_PLAYER_LEVELUP`).
  - Alınamaz durumda olan bir ödüle tıklandığında hata sesi (örn: `ENTITY_VILLAGER_NO`).
- **Mesajlar:** Tüm mesajlar `messages.yml` veya `config.yml` içinde tamamen yapılandırılabilir olmalı. Mesajlarda modern HEX renk desteği bulunmalı. "Ödülünüzü başarıyla aldınız!", "Bu ödülü almak için yeterli yetkiniz yok!" veya "Bu ödülü zaten aldınız, lütfen yarın tekrar deneyin." gibi mesajlar profesyonel bir dille yazılmalı.

Lütfen bana bu eklentinin `Main`, `MenuListener`, `DataManager` ve örnek bir `config.yml` sınıflarını içeren eksiksiz kodlarını sağla. Kodlar 1.21+ metodlarına (örn: eski Material isimleri veya deprecated metodlar kullanılmadan) uygun olsun.