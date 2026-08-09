# Les règles spécifiques au projet sont ajoutées ici.
#
# SQLCipher utilise plusieurs classes accédées indirectement par sa couche
# native. Elles doivent rester disponibles lorsque R8 sera activé.
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
