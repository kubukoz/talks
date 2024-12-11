{ mkSbtDerivation, lib, which, clang, hidapi, PATHS }:

let
  pname = "hidapi-demo";
  buildInputs = [ which clang hidapi ];

in mkSbtDerivation ({
  inherit pname;
  version = "0.1.0";
  depsSha256 = "sha256-FscWGui2ygAebIpmFcBSFv7W84twe2aY7chJ4keMc5k=";
  inherit buildInputs;

  depsWarmupCommand = ''
    sbt compile
  '';
  overrideDepsAttrs = final: prev: { inherit buildInputs; } // PATHS;

  src = with lib.fileset;
    toSource {
      root = ./.;
      fileset = unions [ ./build.sbt ./project ./src ];
    };

  buildPhase = ''
    sbt root/nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp target/scala-3.5.2/native/com.kubukoz.hidapidemo.demo $out/bin/hidapi-demo
  '';
} // PATHS)
