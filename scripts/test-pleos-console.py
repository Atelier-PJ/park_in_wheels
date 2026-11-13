#!/usr/bin/env python3
"""Regression checks for the actual inactive-console failure observed on the AVD."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("console_check", Path(__file__).with_name("check-pleos-console.py"))
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


class ConsoleReadinessTest(unittest.TestCase):
    def test_no_browser_means_simulation_cannot_confirm_light(self):
        state = {"browser": {"active": False, "clients": 0}, "body": {"emulator_active": False, "dds_ready": {"ambient_color": False, "ambient_actual_color": False}}}
        self.assertEqual(len(check.readiness(state)), 4)

    def test_browser_alone_is_not_enough_without_actual_color_publisher(self):
        state = {"browser": {"active": True, "clients": 1}, "body": {"emulator_active": True, "dds_ready": {"ambient_color": True, "ambient_actual_color": False}}}
        self.assertEqual(check.readiness(state), ["조명 DDS 미준비: ambient_actual_color"])

    def test_connected_live_publishers_are_ready(self):
        state = {"browser": {"active": True, "clients": 1}, "body": {"emulator_active": True, "dds_ready": {"ambient_color": True, "ambient_actual_color": True}}}
        self.assertEqual(check.readiness(state), [])


if __name__ == "__main__":
    unittest.main()
