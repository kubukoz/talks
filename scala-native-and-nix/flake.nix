{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";
  inputs.sbt-derivation.url = "github:zaninime/sbt-derivation";
  inputs.sn-bindgen.url = "github:indoorvivants/sn-bindgen";

  outputs = { nixpkgs, flake-utils, sbt-derivation, sn-bindgen, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays =
            [ sbt-derivation.overlays.default sn-bindgen.overlays.default ];
        };
        PATHS = {
          BINDGEN_PATH = pkgs.lib.getExe pkgs.sn-bindgen;
          HIDAPI_PATH = "${pkgs.hidapi}/include/hidapi/hidapi.h";
        };
      in {
        devShells.default = pkgs.mkShell
          ({ nativeBuildInputs = [ pkgs.hidapi pkgs.sn-bindgen ]; } // PATHS);
        packages.default = pkgs.callPackage ./derivation.nix { inherit PATHS; };
      });
}
