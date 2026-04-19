# DotSave

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.md)
[![es](https://img.shields.io/badge/lang-es-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.es.md)

Una herramienta sencilla para respaldar y restaurar archivos dot (archivos de configuración en Linux) para el sistema operativo Linux.

## Características

- Especifique la estructura del respaldo en uno o varios archivos de configuración.
- Haga un respaldo de los archivos configurados en el directorio del archivo de configuración.
- Restaure los archivos configurados desde el directorio del archivo de configuración.
- Sustitución de variables de entorno en rutas y nombres.

## Instalación

DotSave requiere que el JRE esté instalado en su sistema para funcionar. Consulte con su distribución cuál es la mejor forma de instalar en su sistema operativo. Después de eso simplemente:

1. Descargue la última versión.
2. Descomprímalo.
3. Agregue el directorio a su variable `PATH`.
4. ???
5. ¡Ganancia!

## Uso

### Empezando

1. Primero necesita un lugar para guardar su respaldo:

   ```bash
   mkdir ~/my-backup
   ```

2. A continuación, debe crear un archivo de configuración (por ejemplo `~/my-backup/home.json`) en su editor favorito:

   ```json
   {
     "profiles": [
       {
         "name": "Home Base",
         "default": true,
         "root": "${HOME}",
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

   Cada `perfil` almacenará todos los archivos y directorios especificados en `include`, excepto por los archivos y directorios especificados en `ignore`, en el directorio padre del archivo de configuración y el nombre del perfil. Por ejemplo, el JSON anterior almacenará su perfil en `~/my-backup/Home Base`. Los archivos y directorios deben conservar su dueño y sus permisos. La herramienta también revisará el directorio raíz del perfil por archivos no incluidos o ignorados y mostrará una advertencia que esos archivos serán implícitamente ignorados hasta que sean explícitamente incluidos o ignorados. De esta manera puede descubrir si un nuevo archivo fue agregado por una aplicación existente o nueva.

   > [!WARNING]
   > Si ignora un directorio y más tarde un archivo es agregado a ese directorio, la herramienta no podrá detectar esto. Asegúrese de que no serán agregados archivos o que los archivos agregados nunca serán relevantes.

   > [!NOTE]
   > Note que la variable de entorno `HOME` fue usada en la propiedad `root` del perfil. Durante la ejecución, la variable será evaluada y reemplazada. Puede usar variables de entorno en todas las propiedades de tipo cadena (string) con la notación `${NAME}`.

3. ¡Ahora simplemente ejecute la herramienta con la opción `-b` para respaldar!

   ```bash
   dotsave -b ~/my-backup/home.json
   ```

4. Para restaurar, simplemente use la opción `-r`:

   ```bash
   dotsave -r ~/my-backup/home.json
   ```

> [!IMPORTANT]
> ¡Puede crear un archivo de configuración aparte que haga una respaldo de los archivos propiedad de root y usar `sudo` solo con ese archivo de configuración!

### Perfiles anidados

A veces uno puede tener multiples dispositivos que quiere compartan cierta configuración entre ellos, pero no toda. Puede agregar más perfiles al archivo de configuración y componer un perfil a partir de otros con `includeProfiles` y `inheritProfiles`. Por ejemplo:

```json
{
  "profiles": [
    {
      "name": "Neofetch",
      "root": "${HOME}/.config/neofetch",
      "include": [
        "config.conf"
      ],
      "ignore": []
    },
    {
      "name": "FreeCAD",
      "root": "${HOME}/.config/FreeCAD",
      "include": [
        "FreeCAD.conf",
        "system.cfg",
        "user.cfg"
      ],
      "ignore": []
    },
    {
      "name": "BatteryMonitor",
      "root": "${HOME}/.config/BatteryMonitor",
      "include": [
        "config.conf"
      ],
      "ignore": []
    },
    {
      "name": "KDE",
      "root": "${HOME}/.config",
      "include": [
        "kde.org/*"
      ],
      "ignore": []
    },
    {
      "name": "Shared",
      "root": "${HOME}",
      "inheritProfiles": [
        "Neofetch"
      ],
      "include": [],
      "ignore": []
    },
    {
      "name": "Desktop",
      "default": true,
      "root": "${HOME}",
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
      "root": "${HOME}",
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

Asumiendo que ejecutamos el perfil `Desktop` con `dotsave -b ~/my-backup/home.json -p Desktop`:

1. El perfil `Desktop` va a ejecutar primero los perfiles en la lista `includeProfiles`, en este caso `Shared`. Por lo que tendremos un directorio `~/my-backup/Shared`.
2. El perfil `Shared` no tiene nada en `includeProfiles` así que se brincará este paso. En vez de eso, va a fusionar consigo mismo los perfiles en `inheritProfiles`, en este caso `Neofetch`. Así que tendremos un archivo respaldado en `~/my-backup/Shared/.config/neofetch/config.conf`. Note que no hay un directorio superior de "Neofetch", sino que está adentro de "Shared".
3. Después de ejecutar `Shared`, `Desktop` va a fusionar los perfiles en `inheritProfiles` que son `FreeCAD` y `KDE`. asi que se creará un directorio `~/my-backup/Desktop` y se respaldarán archivos como `~/my-backup/Desktop/.config/FreeCAD/FreeCAD.conf` y `~/my-backup/Desktop/.config/kde.org/plasmashell.conf`.

> [!NOTE]
> Note que ambos perfiles `Desktop` y `Laptop` hacen **inherit** a `KDE`, pero los respaldos de KDE serán guardados en diferentes directorios, asi que los contenidos de los archivos pueden ser diferentes. Igualmente, ambos hacen **include** a `Shared`, que será guardado en el mismo directorio `Shared` y por ende será compartido entre los dos perfiles.

> [!IMPORTANT]
> Puede marcar un perfil como perfil por defecto con la propiedad `default` para que no tenga que especificar el nombre del perfil en la línea de comandos. Solo **un** perfil puede ser marcado como por defecto.

> [!TIP]
> Puede versionar sus respaldos usando `git` en caso de que quiera regresar a una configuración anterior.

### Otras opciones

Estos son otros argumentos de línea de comandos que puede usar:

- `-v` o `--verbose` - Activa el modo verboso para tener una idea más detallada de lo que la herramienta está haciendo.
- `-d` o `--dry-run` - Activa el modo simulación donde no se realizarán cambios al sistema de archivos. Muy útil cuando se junta con el modo verboso.

## Licencia

Este proyecto está bajo la licencia [GNU GPL-3.0](https://choosealicense.com/licenses/gpl-3.0/).

## Contribuyendo

- Si tiene problemas con DotSave, abra un informe de error con información detallada sobre cómo reproducir el problema, e intentaré arreglarlo.
- Si desea mejorar DotSave, envíe un PR con sus cambios y los revisaré.

## Cómprame un café

Siempre puede invitarme un café aquí:

[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg?logo=paypal&style=for-the-badge)](https://www.paypal.com/donate/?business=AKVCM878H36R6&no_recurring=0&item_name=Buy+me+a+coffee&currency_code=USD)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Donate-blue.svg?logo=kofi&style=for-the-badge)](https://ko-fi.com/jurgencruz)
