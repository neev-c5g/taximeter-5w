#!/system/bin/sh
CLASSPATH="$HMGAPI_MOD_DIR/svc/speed.jar:/system/framework/hmgapi-hook.jar"
export CLASSPATH
exec /system/bin/app_process /system/bin hmgapi.vehicle.TaxiSpeedHub
