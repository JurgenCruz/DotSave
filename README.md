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

DotSave requires the JRE to be installed in your system to work. Check with your distro what is the best way to install it in your OS. After that just:

1. Download the latest release.
2. Unzip it.
3. Add the directory to your `PATH` variable.
4. ???
5. Profit!

## Usage

### Getting Started

1. First you need a place to put your backup:

   ```bash
   mkdir ~/my-backup
   ```

2. Next you need to make a config file (for example `~/my-backup/home.json`) in your favorite editor:

   ```json
   {
     "profiles": [
       {
         "name": "Home Base",
         "default": true,
         "root": "$HOME",
         "include": [
           ".config/neofetch/config.conf"
         ],
         "ignore": [
           ".npm/"
         ]
       }
     ]
   }
   ```

   Each `profile` will store all the files and directories specified in `include`, except for files and directories specified in `ignore`, in the parent directory of the config file and the name of the profile. For example, the JSON above will store its profile in `~/my-backup/Home Base`. Files and directories should preserve their owner and permissions. The tool will also check the root directory of the profile for files not included or ignored and will issue a warning that those files will be implicitly ignored until explicitly included or ignored. This way you can discover if a new file was added by an existing or new app.

   > [!WARNING]
   > If you ignore a directory and later a file is added to that directory, the tool won't be able to detect this. Be sure files won't be added or that files added to the directory won't ever matter.

3. Now just execute the tool with the `-b` option to back up!

   ```bash
   dotsave -b ~/my-backup/home.json
   ```

4. To restore, just use the `-r` option:

   ```bash
   dotsave -r ~/my-backup/home.json
   ```

> [!IMPORTANT]
> You can create a separate config file that backups files owned by root and use `sudo` just with that config file!

### Nested profiles

Sometimes you may have multiple devices which you want to share certain configuration between them, but not all. You can add more profiles to the config file and compose a profile from other using `includeProfiles` and `inheritProfiles`. For example:

```json
{
  "profiles": [
    {
      "name": "Neofetch",
      "root": "$HOME/.config/neofetch",
      "include": [
        "config.conf"
      ],
      "ignore": []
    },
    {
      "name": "FreeCAD",
      "root": "$HOME/.config/FreeCAD",
      "include": [
        "FreeCAD.conf",
        "system.cfg",
        "user.cfg"
      ],
      "ignore": []
    },
    {
      "name": "BatteryMonitor",
      "root": "$HOME/.config/BatteryMonitor",
      "include": [
        "config.conf"
      ],
      "ignore": []
    },
    {
      "name": "KDE",
      "root": "$HOME/.config",
      "include": [
        "kde.org/*"
      ],
      "ignore": []
    },
    {
      "name": "Shared",
      "root": "$HOME",
      "inheritProfiles": [
        "Neofetch"
      ],
      "include": [],
      "ignore": []
    },
    {
      "name": "Desktop",
      "default": true,
      "root": "$HOME",
      "includeProfiles": [
        "Shared"
      ],
      "inheritProfiles": [
        "FreeCAD",
        "KDE"
      ],
      "include": [],
      "ignore": [
        ".gradle/"
      ]
    },
    {
      "name": "Laptop",
      "root": "$HOME",
      "includeProfiles": [
        "Shared"
      ],
      "inheritProfiles": [
        "BatteryMonitor",
        "KDE"
      ],
      "include": [],
      "ignore": []
    }
  ]
}
```

Let's assume we run the `Desktop` profile with `dotsave -b ~/my-backup/home.json -p Desktop`:

1. `Desktop` profile will first execute the profiles in the `includeProfiles` list, in this case `Shared`. So we will end with a directory `~/my-backup/Shared`.
2. `Shared` profile doesn't have any `includeProfiles` so it will skip that step. Instead, it will merge in itself all the profiles in `inheritProfiles`, in this case `Neofetch`. so we will have a file backed up at `~/my-backup/Shared/.config/neofetch/config.conf`. Notice there is no top level "Neofetch" directory, is it inside "Shared".
3. After executing `Shared`, `Desktop` will merge the profiles in `inheritProfiles` which are `FreeCAD` and `KDE`. So a `~/my-backup/Desktop/` directory will be created and back up the files like `~/my-backup/Desktop/.config/FreeCAD/FreeCAD.conf` and `~/my-backup/Desktop/.config/kde.org/plasmashell.conf`.

> [!NOTE]
> Note that both `Desktop` and `Laptop` profiles **inherit** `KDE`, but the KDE backups will be stored in different directories, so the contents of the files can be different. Likewise, both of them **include** `Shared`, which will be stored in the same `Shared` directory and thus shared between the two profiles.

> [!IMPORTANT]
> You can mark a profile as the default with the `default` property so you don't need to specify the profile name in the command line. Only **one** profile can be marked as default.

> [!TIP]
> You can version your backups using `git` in case you ever want to go back to a previous configuration.

## License

This project is under the [GNU GPL-3.0](https://choosealicense.com/licenses/gpl-3.0/) license.

## Contributing

- If you have issues with DotSave, please open a bug report with as detailed information on how to reproduce the issue, and I'll try to fix it.
- If you wish to improve DotSave, please submit a PR with your changes and I will review them.

## Buy me a coffee

You can always buy me a coffee here:

[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg?logo=paypal&style=for-the-badge)](https://www.paypal.com/donate/?business=AKVCM878H36R6&no_recurring=0&item_name=Buy+me+a+coffee&currency_code=USD)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Donate-blue.svg?logo=kofi&style=for-the-badge)](https://ko-fi.com/jurgencruz)
