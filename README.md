# 택시미터기 1.0.1

전체 화면에서 운행과 지역 요금을 설정하고 운행기록을 조회합니다. [분할화면 커스텀](https://github.com/neev-c5g/custom-split-widgets)을 함께 설치하면 오른쪽 위젯에서 같은 운행 상태를 확인하고 앱으로 이동할 수 있습니다. 앱에서 변경한 지역은 다음 운행과 위젯에 반영됩니다.

- [NEMESIS USB 설치 ZIP](taximeter-experiment-1.0.1-nemesis.zip)
- [NEMESIS 패키지 ZIP](taximeter-experiment-1.0.1-nemesis-package.zip)
- [파일 무결성 SHA-256](SHA256SUMS)
- [지역 요금 출처와 계산 범위](fare-sources.md)

HMG-API 1.0.2 이상이 필요합니다. USB 설치 ZIP은 압축을 풀어 USB의 최상위에 복사합니다. 다른 설치 패키지와 `NEMESIS.data`를 덮어쓰지 않도록 각각 따로 설치하세요. 설치 후 차량 앱 목록에서 **택시미터기**를 열고, 분할화면 커스텀에서 택시미터기 위젯을 선택하세요.

요금은 차속 샘플을 이용한 추정치이며 인증 택시미터기가 아닙니다. 지역별 복합할증과 세부 조건이 모두 반영되지는 않으며 실제 청구 요금과 다를 수 있습니다. 정차 상태에서 위젯 표시, 앱 전환, 운행 시작·종료 및 기록 조회를 확인했습니다. 실제 주행 중 거리 정확도와 속도에 따른 애니메이션은 확인하지 못했습니다. NEMESIS USB 설치는 패키지 구조만 확인했으며 차량에서 직접 설치하지 않았습니다.

`app`, `bridge`, `src`, `res`, `tests`는 해당 버전의 소스입니다. 빌드에는 Android SDK와 HMG-API 소스, 분할화면 SDK 및 별도로 보관한 서명 키가 필요합니다.
