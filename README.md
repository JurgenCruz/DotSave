# DotSave

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.md)
[![es](https://img.shields.io/badge/lang-es-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.es.md)

A simple tool to back up and restore dot files (config files in Linux) for Linux OS.

## Features

- Specify backup structure in one or multiple config files.
- Back up the configured files to the directory of the config file.
- Restore the configured files from the directory of the config file.
- Environment Variable substitution in paths and names.

## Installation

DotSave requires the JRE to be installed in your system to work. Check with your distro what is the best way to install
it in your OS. After that just:

1. Download the latest release.
2. Unzip it.
3. Add the directory to your `PATH` variable.
4. ???
5. Profit!

## Usage

1. First you need a place to put your backup:

   ```bash
   mkdir ~/my-backup
   ```

2. Next you need to make a config file (for example `~/my-backup/apps.json`) in your favorite editor:

   ```json
   {
     "profiles": [
       {
         "name": "neofetch",
         "root": "$HOME/.config/neofetch",
         "include": [
           "config.conf"
         ]
       }
     ]
   }
   ```

   Each `profile` will store all the files and directories specified in `include` in the parent directory of the config
   file and the name of the profile. For example, the json above will store its profile in `~/my-backup/neofetch`. Files
   and directories should preserve their owner and permissions.

3. Now just execute the tool with the `-b` option to back up!

   ```bash
   dotsave -b ~/my-backup/apps.json
   ```

4. To restore, just use the `-r` option:

   ```bash
   dotsave -r ~/my-backup/apps.json
   ```

> [!TIP]
> You can create a separate config file that backups files owned by root and use `sudo` just with that config file!

## License

This project is under the [GNU GPL-3.0](https://choosealicense.com/licenses/gpl-3.0/) license.

## Contributing

- If you have issues with DotSave, please open a bug report with as detailed information on how to reproduce the issue,
  and I'll try to fix it.
- If you wish to improve DotSave, please submit a PR with your changes and I will review them.

## Buy me a coffee

You can always buy me a coffee here:

[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg?logo=paypal&style=for-the-badge)](https://www.paypal.com/donate/?business=AKVCM878H36R6&no_recurring=0&item_name=Buy+me+a+coffee&currency_code=USD)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Donate-blue.svg?logo=kofi&style=for-the-badge)](https://ko-fi.com/jurgencruz)
