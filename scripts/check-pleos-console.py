#!/usr/bin/env python3
"""Read-only readiness check. Never injects DDS values or sends fake success callbacks."""
import argparse
import json
import sys
import urllib.request


def readiness(state):
    browser = state.get("browser", {})
    body = state.get("body", {})
    ready = body.get("dds_ready", {})
    errors = []
    if not browser.get("active") or browser.get("clients", 0) < 1:
        errors.append("Virtual Console 브라우저 연결 없음")
    if not body.get("emulator_active"):
        errors.append("차량 DDS 시뮬레이션 비활성")
    for key in ("ambient_color", "ambient_actual_color"):
        if not ready.get(key):
            errors.append(f"조명 DDS 미준비: {key}")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://127.0.0.1:8088")
    args = parser.parse_args()
    try:
        with urllib.request.urlopen(args.url.rstrip("/") + "/vehicle/state", timeout=5) as response:
            state = json.load(response)
        errors = readiness(state)
    except (OSError, ValueError) as error:
        errors = [f"Virtual Console 조회 실패: {error}"]
    if errors:
        print("FAIL: " + "; ".join(errors), file=sys.stderr)
        print(f"Pleos AVD에서 adb forward tcp:8088 tcp:8088 후 {args.url}/ 를 브라우저에 열어 두세요.", file=sys.stderr)
        return 1
    print("PASS: Virtual Console connected; actual ambient DDS publishers ready")
    return 0


if __name__ == "__main__":
    sys.exit(main())
