// cmd/keys/add.go
package keys

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/Riviya/Keypilot/cli/client")

func newAddCmd(gatewayURL *string) *cobra.Command {
	var provider string
	var keyValue string

	cmd := &cobra.Command{
		Use:   "add",
		Short: "Add a new API key to the gateway",
		Example: `  gateway-cli keys add --provider openai --key sk-abc123
  gateway-cli keys add --provider anthropic --key sk-ant-xyz`,
		RunE: func(cmd *cobra.Command, args []string) error {
			if provider == "" {
				return fmt.Errorf("--provider is required")
			}
			if keyValue == "" {
				return fmt.Errorf("--key is required")
			}

			c := client.NewGatewayClient(*gatewayURL)
			resp, err := c.AddKey(provider, keyValue)
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error: %v\n", err)
				os.Exit(1)
			}

			fmt.Printf("✅ Key added successfully\n")
			fmt.Printf("   ID:       %s\n", resp.ID)
			fmt.Printf("   Provider: %s\n", resp.Provider)
			fmt.Printf("   Active:   %v\n", resp.Active)
			return nil
		},
	}

	cmd.Flags().StringVar(&provider, "provider", "", "AI provider name (e.g. openai, anthropic)")
	cmd.Flags().StringVar(&keyValue, "key", "", "The API key value")

	return cmd
}