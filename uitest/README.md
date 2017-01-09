AWS Device Farm (Appium Automated Testing)
---

=== Install Dependencies

Check out http://docs.python-guide.org/en/latest/dev/virtualenvs/ for instructions

1. appium (version 1.6.3 is recommended)
  npm install -g appium --no-shrinkwrap


1. virtualenvwrapper
  Put these in your ~/.bashrc or ~/.bash_profile
    export WORKON_HOME=~/.virtualenvs
    source /usr/local/bin/virtualenvwrapper.sh

  Go to uitest directory, then execute command below
    mkvirtualenv bard-android-test

2. rvm

  this will make ".rvmrc" to get autoexecuted everytime you cd into uitest folder
  .rvmrc contains
    workon bard-android-test

=== Setup

pip install -r requirements.txt


=== Testing Locally

appium --debug-log-spacing --automation-name "Appium" --platform-name "Android" --session-override

python tests/create_and_save_test.py


=== Testing Remotely (AWS Device Farm)

py.test --collect-only tests/
pip freeze > requirements.txt
pip wheel --wheel-dir wheelhouse -r requirements.txt
zip -r test_bundle.zip tests/ wheelhouse/ requirements.txt

upload test_bundle.zip to test run
