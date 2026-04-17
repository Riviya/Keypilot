// cmd/status.go
package cmd

import (
	"fmt"
	"os"
	"strings"

	"github.com/spf13/cobra"
	
	"github.com/Riviya/Keypilot/cli/client"

)

func newStatusCmd(gatewayURL *string) *cobra.Command {
	return &cobra.Command{
		Use:   "status",
		Short: "Show the live status of the gateway",
		RunE: func(cmd *cobra.Command, args []string) error {
			c := client.NewGatewayClient(*gatewayURL)
			status, err := c.GetStatus()
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error: could not reach gateway at %s\n", *gatewayURL)
				fmt.Fprintf(os.Stderr, "Is the gateway running? Start it with: ./mvnw spring-boot:run\n")
				os.Exit(1)
			}

			healthSymbol := "✅"
			if !status.Healthy {
				healthSymbol = "❌"
			}
			fmt.Printf("%s Gateway: %s\n\n", healthSymbol, *gatewayURL)

			if len(status.Providers) == 0 {
				fmt.Println("No providers configured.")
				fmt.Println("Add providers to application.properties and restart the gateway.")
				return nil
			}

			fmt.Printf("%-12s  %-10s  %-6s  %-9s  %-8s  %-8s\n",
				"PROVIDER", "STATUS", "TOTAL", "AVAILABLE", "LIMITED", "INACTIVE")
			fmt.Println(strings.Repeat("-", 65))

			for _, p := range status.Providers {
				statusLabel := "available"
				if !p.Available {
					statusLabel = "degraded"
				}
				fmt.Printf("%-12s  %-10s  %-6d  %-9d  %-8d  %-8d\n",
					p.ProviderName,
					statusLabel,
					p.TotalKeys,
					p.AvailableKeys,
					p.RateLimitedKeys,
					p.InactiveKeys,
				)
			}

			return nil
		},
	}
}