// cmd/root.go
package cmd

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/Riviya/Keypilot/cli/cmd/keys"
)

var gatewayURL string

var rootCmd = &cobra.Command{
	Use:   "gateway-cli",
	Short: "CLI tool for managing the local AI API gateway",
	Long: `gateway-cli lets you add, list, and delete API keys
managed by your local API gateway running on localhost.`,
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func init() {
	rootCmd.PersistentFlags().StringVar(
		&gatewayURL,
		"gateway",
		"http://localhost:4000",
		"Base URL of the local gateway",
	)

	// Register the "keys" subcommand group
	rootCmd.AddCommand(keys.NewKeysCmd(&gatewayURL))
}