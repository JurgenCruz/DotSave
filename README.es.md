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
         "root": "$HOME",
         "include": [
           ".config/neofetch/config.conf"
         ],
         "exclude": [
           ".npm/"
         ]
       }
     ]
   }
   ```

   Cada `perfil` almacenará todos los archivos y directorios especificados en `include`, excepto por los archivos y directorios especificados en `exclude`, en el directorio padre del archivo de configuración y el nombre del perfil. Por ejemplo, el JSON anterior almacenará su perfil en `~/my-backup/Home Base`. Los archivos y directorios deben conservar su dueño y sus permisos. La herramienta también revisará el directorio raíz del perfil por archivos no incluidos o excluidos and mostrará una advertencia que esos archivos serán implícitamente excluidos hasta que sean explícitamente incluidos o excluidos. De esta manera puede descubrir si un nuevo archivo fue agregado por una aplicación existente o nueva.

   > [!WARNING]
   > Si excluye un directorio y más tarde un archivo es agregado a ese directorio, la herramienta no podrá detectar esto. Asegúrese de que no serán agregados archivos o que los archivos agregados nunca serán relevantes.

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

A veces uno puede tener multiples dispositivos que quiere compartan cierta configuración entre ellos, pero no toda. Puede agregar más perfiles al archivo de configuración y asignar un perfil padre con `parent` en vez de especificar un directorio raíz con `root`. Por ejemplo:

```json
{
  "profiles": [
    {
      "name": "Home Base",
      "root": "$HOME",
      "include": [
        ".config/neofetch/config.conf"
      ],
      "exclude": [
        ".npm/"
      ]
    },
    {
      "name": "Desktop",
      "default": true,
      "parent": "Home Base",
      "include": [
        ".config/audacious/config.conf"
      ],
      "exclude": [
        ".gradle/"
      ]
    },
    {
      "name": "Laptop",
      "parent": "Home Base",
      "include": [
        ".config/audacious/config.conf"
      ],
      "exclude": [
        ".gradle/"
      ]
    }
  ]
}
```

Y ejecute con `dotsave -b ~/my-backup/home.json -p Desktop`.

El perfil `Desktop` entonces ejecutará primero el perfil padre y luego el mismo. Para los propósitos de detección de archivos faltantes, las listas de inclusión y exclusión serán fusionadas. Esto significa que es valido que el perfil padre tenga archivos faltantes si se ejecuta solo si piensa cubrir estos archivos en el perfil hijo.

> [!NOTE]
> Note que ambos perfiles `Desktop` y `Laptop` son iguales, pero los respaldos serán guardados en diferentes directorios, asi que los contenidos de los archivos pueden ser diferentes.

> [!IMPORTANT]
> Puede marcar un perfil como perfil por defecto con la propiedad `default` para que no tenga que especificar el nombre del perfil en la línea de comandos. Solo **un** perfil puede ser marcado como por defecto.

> [!TIP]
> Puede versionar sus respaldos usando `git` en caso de que quiera regresar a una configuración anterior.

## Licencia

Este proyecto está bajo la licencia [GNU GPL-3.0](https://choosealicense.com/licenses/gpl-3.0/).

## Contribuyendo

- Si tiene problemas con DotSave, abra un informe de error con información detallada sobre cómo reproducir el problema, e intentaré arreglarlo.
- Si desea mejorar DotSave, envíe un PR con sus cambios y los revisaré.

## Cómprame un café

Siempre puede invitarme un café aquí:

[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg?logo=paypal&style=for-the-badge)](https://www.paypal.com/donate/?business=AKVCM878H36R6&no_recurring=0&item_name=Buy+me+a+coffee&currency_code=USD)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Donate-blue.svg?logo=kofi&style=for-the-badge)](https://ko-fi.com/jurgencruz)
