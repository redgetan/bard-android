import os
from time import sleep

import unittest

from appium import webdriver

# Returns abs path relative to this file and not cwd
PATH = lambda p: os.path.abspath(
    os.path.join(os.path.dirname(__file__), p)
)

class SimpleAndroidTests(unittest.TestCase):
    def setUp(self):
        desired_caps = {}
        desired_caps['platformName'] = 'Android'
        desired_caps['deviceName'] = 'Android Phone'
        desired_caps['newCommandTimeout'] = '0' # no timeout
        desired_caps['app'] = PATH(
            './../../app/build/outputs/apk/app-dev-debug.apk'
        )

        self.driver = webdriver.Remote('http://localhost:4723/wd/hub', desired_caps)

    def tearDown(self):
        # end the session
        self.driver.quit()

    def test_find_elements(self):
        elements = self.driver.find_elements_by_accessibility_id('Allow')
        if len(elements) > 0:
            elements[0].click()
            sleep(2)

        # click first scene
        self.driver.find_element_by_id("com.roplabs.bard:id/scene_title").click()
        sleep(1)

        # import pdb; pdb.set_trace()

        # click wordTags
        self.driver.find_element_by_id("com.roplabs.bard:id/word_tag").click()
        sleep(1)
        self.driver.find_element_by_id("com.roplabs.bard:id/word_tag").click()
        sleep(1)
        self.driver.find_element_by_id("com.roplabs.bard:id/word_tag").click()
        sleep(1)

        # click "Talk"
        self.driver.find_element_by_id("com.roplabs.bard:id/play_message_btn").click()
        sleep(5)

        # import pdb; pdb.set_trace()

        screenshot_folder = os.getenv('SCREENSHOT_PATH') or os.getcwd() + "/screenshots"
        self.driver.save_screenshot(screenshot_folder + "/merged_video.png")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(SimpleAndroidTests)
    unittest.TextTestRunner(verbosity=2).run(suite)
