# DotSave

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.md)
[![es](https://img.shields.io/badge/lang-es-blue.svg)](https://github.com/jurgencruz/dotsave/blob/master/README.es.md)

Una herramienta sencilla para realizar copias de seguridad y restaurar archivos dot (archivos de configuración en Linux)
para el sistema operativo Linux.

## Características

- Especifique la estructura de la copia de seguridad en uno o varios archivos de configuración.
- Haga una copia de seguridad de los archivos configurados en el directorio del archivo de configuración.
- Restaure los archivos configurados desde el directorio del archivo de configuración.
- Sustitución de variables de entorno en rutas y nombres.

## Instalación

DotSave requiere que el JRE esté instalado en su sistema para funcionar. Consulte con su distribución cuál es la mejor
forma de instalar en su sistema operativo. Después de eso simplemente:

1. Descargue la última versión.
2. Descomprímalo.
3. Agregue el directorio a su variable `PATH`.
4. ???
5. ¡Beneficio!

## Uso

1. Primero necesita un lugar para guardar su copia de seguridad:

   ```bash
   mkdir ~/my-backup
   ```

2. A continuación, debe crear un archivo de configuración (por ejemplo `~/my-backup/apps.json`) en su editor favorito:

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

   Cada `perfil` almacenará todos los archivos y directorios especificados en `include` en el directorio padre del
   archivo de configuración y el nombre del perfil. Por ejemplo, el json anterior almacenará su perfil en
   `~/my-backup/neofetch`. Los archivos y directorios deben conservar su dueño y sus permisos.

3. ¡Ahora simplemente ejecute la herramienta con la opción `-b` para realizar una copia de seguridad!

   ```bash
   dotsave -b ~/my-backup/apps.json
   ```

4. Para restaurar, simplemente use la opción `-r`:

   ```bash
   dotsave -r ~/my-backup/apps.json
   ```

> [!TIP]
> ¡Puede crear un archivo de configuración aparte que haga una copia de seguridad de los archivos propiedad de root y
> usar `sudo` solo con ese archivo de configuración!

## Licencia

Este proyecto está bajo la licencia [GNU GPL-3.0](https://choosealicense.com/licenses/gpl-3.0/).

## Contribuyendo

- Si tiene problemas con DotSave, abra un informe de error con información detallada sobre cómo reproducir el problema,
  e intentaré arreglarlo.
- Si desea mejorar DotSave, envíe un PR con sus cambios y los revisaré.

## Cómprame un café

Siempre puede invitarme un café aquí:

[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg?logo=paypal&style=for-the-badge)](https://www.paypal.com/donate/?business=AKVCM878H36R6&no_recurring=0&item_name=Buy+me+a+coffee&currency_code=USD)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Donate-blue.svg?logo=kofi&style=for-the-badge)](https://ko-fi.com/jurgencruz)
