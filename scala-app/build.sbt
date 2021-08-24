lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion    = "2.6.10"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "ipogudin",
      scalaVersion    := "2.13.3"
    )),
    name := "scala-app",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "org.jsoup"         % "jsoup"                     % "1.13.1",
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "com.typesafe"      % "config"                    % "1.4.0" force(),
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )
