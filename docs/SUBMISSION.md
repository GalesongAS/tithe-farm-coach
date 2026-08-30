# Plugin Hub submission checklist

## Plugin repository

1. Create `https://github.com/GalesongAS/tithe-farm-coach` as a public repository.
2. Push this project without `.gradle`, `build`, IDE files, or JAR files.
3. Keep the public author in `runelite-plugin.properties` set to `GalesongAS`.
4. Run `gradlew.bat clean build` on Windows or `./gradlew clean build` elsewhere.
5. Run the development client with `gradlew.bat run` and manually test every
   method. A successful build does not prove in-game behavior.
6. Commit and push the tested version.

## Plugin Hub pull request

1. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
2. Create `plugins/tithe-farm-coach` in the fork with:

   ```text
   repository=https://github.com/GalesongAS/tithe-farm-coach.git
   commit=FULL_40_CHARACTER_COMMIT_HASH
   ```

3. Open one pull request to `runelite/plugin-hub` and keep subsequent fixes in
   that same pull request.
4. Address both the build workflow and RuneLite Plugin Hub Checks results.

## Suggested pull-request summary

> Tithe Farm Coach is a read-only skilling overlay. It observes Tithe Farm plant
> objects and inventory state to show one suggested plant, water, harvest,
> deposit, or refill action. It does not inject input, alter menus, communicate
> with external services, or write gameplay logs.

The Plugin Hub maintainers make the final policy and quality determination.
