{
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { nixpkgs, flake-utils, ... }: flake-utils.lib.eachDefaultSystem (system: {
    devShell = let pkgs = import nixpkgs { inherit system; }; in

      pkgs.mkShell {
        packages = [ pkgs.sbt pkgs.coursier pkgs.nodejs ];
      };
  });
}
