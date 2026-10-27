# 형제모델 등록

실제 제품 자료를 받은 뒤 같은 제품 유형의 형제모델을 등록합니다. 데이터 팩의 목록과 클라이언트 외형 리소스를 **함께** 제공해야 합니다. 목록만 추가하면 화면에는 나오지만 외형은 바뀌지 않습니다. 서로 다른 통신 방식이나 기능을 임의로 동등한 제품으로 등록하지 않습니다.

## 서버 목록

`data/<namespace>/sibling_models/<device_block_path>.json` 경로를 사용합니다. 예를 들어 현재 월패드 블록은 `data/homesystems/sibling_models/wallpad.json`입니다. 파일의 식별자가 정확히 등록된 DeviceBlock 식별자와 같아야 합니다.

```json
{
  "models": [
    {"slot": 0, "name": "자료에서 확인한 원본 모델명"},
    {"slot": 1, "name": "자료에서 확인한 형제모델명"}
  ]
}
```

한 제품당 원본 포함 2~16개, 슬롯은 고유한 정수 0~15, 모델명은 1~64자입니다. 원본 슬롯 0은 필수이며 기존 월드 기기의 기본값입니다. 파일을 제거하면 새 선택은 차단하지만 설치된 모델 상태는 유지합니다. 이미 쓰인 슬롯은 다른 모델로 재할당하지 마세요.

목록은 서버 시작과 `/reload` 때 읽습니다. 잘못된 파일은 경고와 함께 제외합니다. 다시 로드한 목록이 화면을 열 때의 목록과 다르면 기존 화면에서 변경을 거부하므로 다시 여세요. 서버 목록은 화면 열기 패킷에 담아 클라이언트로 전송하며 클라이언트의 임의 목록으로 선택할 수 없습니다.

## 외형 리소스

제품 블록에 `model=0..15` 상태가 추가되었습니다. 블록 자체를 교체하지 않으므로 주소·설치자·장치 UUID·각종 연결이 유지됩니다. 외형 리소스는 설치된 각 클라이언트의 같은 모드/리소스 팩에 포함합니다.

예를 들어 해당 제품의 `assets/homesystems/blockstates/wallpad.json`에서 **모든 네 방향과 모든 슬롯**에 대해 다음처럼 모델을 지정합니다. 등록하지 않은 슬롯도 기본 모델로 매핑하여 누락 텍스처를 방지합니다.

```json
{
  "variants": {
    "facing=north,model=0": {"model": "homesystems:block/actual_original"},
    "facing=north,model=1": {"model": "homesystems:block/actual_sibling"},
    "facing=east,model=0": {"model": "homesystems:block/actual_original", "y": 90},
    "facing=east,model=1": {"model": "homesystems:block/actual_sibling", "y": 90}
  }
}
```

위 JSON은 설명용 일부입니다. 실제 파일에는 남쪽(180), 서쪽(270), 나머지 슬롯도 반드시 포함합니다. `powered`를 조건에서 생략하면 양쪽 전원 상태에 적용됩니다. 모델 JSON은 `assets/homesystems/models/block/`에, 텍스처는 `assets/homesystems/textures/`에 넣습니다. 현재 배포본 기본 블록 상태 파일은 `model`을 구분하지 않으므로 모든 슬롯을 기존 외형으로 표시합니다.

선택기 아이템은 `homesystems:sibling_selector`입니다. 표시 이름과 슬롯 수는 서버가 전달하며 메뉴 클릭은 서버가 소유권·단지·거리·기기 UUID·선택기 소지·통화 상태와 유효 슬롯을 다시 검사합니다. 상태 변경은 일반 블록 동기화로 관찰자에게 전파됩니다. 제품을 부수면 기존과 마찬가지로 기본 아이템이 나오며 다시 설치하면 원본 모델입니다.
