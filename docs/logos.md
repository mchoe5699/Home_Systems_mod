# 제품 로고 교체

모든 기본 기기의 로고는 별도 PNG 리소스입니다. 원래 제조사 로고는 배포본에 포함하지 않았으며 기본값은 HOME SYSTEMS입니다.

리소스 팩에서 다음 파일을 원하는 로고로 바꿉니다. 128×16 투명 PNG를 권장합니다. 같은 로고가 조작 화면과 월드의 기기 전면에 표시됩니다.

```
assets/homesystems/textures/logo/lobby_phone.png
assets/homesystems/textures/logo/video_phone.png
assets/homesystems/textures/logo/interphone.png
assets/homesystems/textures/logo/wallpad.png
assets/homesystems/textures/logo/guard_station.png
assets/homesystems/textures/logo/door_camera.png
assets/homesystems/textures/logo/door_phone.png
assets/homesystems/textures/logo/sht_308.png
```

팩 루트의 `pack.mcmeta`에는 Minecraft 1.20.1용 `pack_format: 15`를 사용합니다. 이 변경은 클라이언트 리소스이므로 모든 플레이어에게 같은 로고를 보이게 하려면 같은 리소스 팩을 적용해야 합니다. 서버 리소스 팩 배포도 가능합니다. 게임 중 리소스 다시 불러오기는 F3+T입니다.

Minecraft는 SVG를 직접 텍스처로 읽지 않습니다. 사용자가 SVG를 제공하면 해당 파일을 PNG로 변환해 이 위치에 등록합니다. PNG/SVG 파일을 제공받기 전에 임의로 특정 회사 로고를 넣지 않습니다.

SHT-308은 사진/PDF에 있는 SAMSUNG 위치의 별도 로고를 사용합니다. 해당 모델은 256×40 투명 PNG를 권장합니다.
