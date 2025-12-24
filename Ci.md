name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up
        run: echo "No build steps defined yet"
      - name: Run basic check
        run: echo "Repository scaffolded successfully"
