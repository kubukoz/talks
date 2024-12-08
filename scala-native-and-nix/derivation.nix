{ mkSbtDerivation, which, clang, hidapi, PATHS }:

let
  pname = "hidapi-demo";
  buildInputs = [ which clang hidapi ];

in mkSbtDerivation {
  inherit pname;
  version = "0.1.0";
  depsSha256 = "sha256-lf0t3hEVdQjaTo4/RFrQC7Bk8nI8iBzL875SA/8viM8=";
  inherit buildInputs;

  depsWarmupCommand = ''
    sbt appNative/compile
  '';
  overrideDepsAttrs = final: prev: {
    inherit buildInputs;
    inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;
  };
  inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;

  src = ./.;

  buildPhase = ''
    sbt appNative/nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp app/native/target/scala-3.5.2/app $out/bin/$pname
  '';
}
